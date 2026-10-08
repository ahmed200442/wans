package com.aistudio.wanas

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.MediaStream
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SoftwareVideoDecoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class WansWebRtcVoiceEngine(
    private val context: Context,
    private val localUserId: String,
    private val sendSignal: suspend (targetUserId: String, type: String, payload: JsonObject) -> Unit
) {
    private val factory: PeerConnectionFactory
    private val audioModule: AudioDeviceModule
    private val audioSource: AudioSource
    private val audioTrack: AudioTrack
    private val peers = mutableMapOf<String, PeerConnection>()
    private var enabled = false

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        audioModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioModule)
            .setVideoEncoderFactory(SoftwareVideoEncoderFactory())
            .setVideoDecoderFactory(SoftwareVideoDecoderFactory())
            .createPeerConnectionFactory()
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("wans-audio-$localUserId", audioSource)
        audioTrack.setEnabled(false)
    }

    fun setMicrophoneEnabled(on: Boolean) {
        enabled = on
        audioTrack.setEnabled(on)
    }

    suspend fun ensurePeer(remoteUserId: String) {
        if (remoteUserId == localUserId || peers.containsKey(remoteUserId)) return
        val connection = createPeer(remoteUserId) ?: return
        peers[remoteUserId] = connection
        if (localUserId < remoteUserId) {
            connection.createOffer(object : BaseSdpObserver() {
                override fun onCreateSuccess(description: SessionDescription) {
                    connection.setLocalDescription(BaseSdpObserver(), description)
                    sendAsync(remoteUserId, "webrtc_offer", buildJsonObject {
                        put("sdp", description.description)
                        put("type", description.type.canonicalForm())
                    })
                }
            }, MediaConstraints())
        }
    }

    fun onOffer(remoteUserId: String, sdp: String) {
        val peer = peers[remoteUserId] ?: createPeer(remoteUserId)?.also { peers[remoteUserId] = it } ?: return
        peer.setRemoteDescription(object : BaseSdpObserver() {
            override fun onSetSuccess() {
                peer.createAnswer(object : BaseSdpObserver() {
                    override fun onCreateSuccess(description: SessionDescription) {
                        peer.setLocalDescription(BaseSdpObserver(), description)
                        sendAsync(remoteUserId, "webrtc_answer", buildJsonObject {
                            put("sdp", description.description)
                            put("type", description.type.canonicalForm())
                        })
                    }
                }, MediaConstraints())
            }
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    fun onAnswer(remoteUserId: String, sdp: String) {
        peers[remoteUserId]?.setRemoteDescription(
            BaseSdpObserver(),
            SessionDescription(SessionDescription.Type.ANSWER, sdp)
        )
    }

    fun onIce(remoteUserId: String, sdpMid: String?, sdpMLineIndex: Int, candidate: String) {
        peers[remoteUserId]?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    fun removePeer(remoteUserId: String) {
        peers.remove(remoteUserId)?.dispose()
    }

    fun release() {
        peers.values.forEach { it.dispose() }
        peers.clear()
        audioTrack.dispose()
        audioSource.dispose()
        audioModule.release()
        factory.dispose()
    }

    private fun createPeer(remoteUserId: String): PeerConnection? {
        val servers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(servers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        return factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                sendAsync(remoteUserId, "webrtc_ice", buildJsonObject {
                    candidate.sdpMid?.let { put("sdp_mid", it) }
                    put("sdp_mline_index", candidate.sdpMLineIndex)
                    put("candidate", candidate.sdp)
                })
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                transceiver?.receiver?.track()?.let { track ->
                    if (track is AudioTrack) track.setEnabled(true)
                }
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onSelectedCandidatePairChanged(event: PeerConnection.CandidatePairChangeEvent?) {}
            override fun onIceCandidateError(event: PeerConnection.IceCandidateErrorEvent?) {}
        })?.also { peer ->
            peer.addTrack(audioTrack)
        }
    }

    private fun sendAsync(target: String, type: String, payload: JsonObject) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { sendSignal(target, type, payload) }
        }
    }

    open class BaseSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) {}
        override fun onSetFailure(error: String) {}
    }
}
