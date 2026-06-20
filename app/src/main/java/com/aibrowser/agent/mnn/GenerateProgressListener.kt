package com.aibrowser.agent.mnn

interface GenerateProgressListener {
    fun onProgress(token: String?): Boolean
}
