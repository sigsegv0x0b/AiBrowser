package com.aibrowser.agent

import com.aibrowser.data.models.AvailableGgufModel

object GgufModelCatalog {
    val models: List<AvailableGgufModel> = listOf(
        AvailableGgufModel(
            id = "llama-3.2-1b-iq4-xs",
            displayName = "Llama 3.2 1B IQ4_XS",
            repo = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            filename = "Llama-3.2-1B-Instruct-IQ4_XS.gguf",
            description = "Smallest 4-bit, great for low-memory devices",
            sizeBytes = 743_000_000L,
            quant = "IQ4_XS",
            contextSize = 131072,
            minRamGb = 2
        ),
        AvailableGgufModel(
            id = "llama-3.2-1b-q4-k-m",
            displayName = "Llama 3.2 1B Q4_K_M",
            repo = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            description = "Recommended 4-bit, good quality for size",
            sizeBytes = 808_000_000L,
            quant = "Q4_K_M",
            contextSize = 131072,
            minRamGb = 2
        ),
        AvailableGgufModel(
            id = "llama-3.2-1b-q5-k-m",
            displayName = "Llama 3.2 1B Q5_K_M",
            repo = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            filename = "Llama-3.2-1B-Instruct-Q5_K_M.gguf",
            description = "High quality 5-bit, near-lossless",
            sizeBytes = 912_000_000L,
            quant = "Q5_K_M",
            contextSize = 131072,
            minRamGb = 3
        ),
        AvailableGgufModel(
            id = "llama-3.2-3b-iq4-xs",
            displayName = "Llama 3.2 3B IQ4_XS",
            repo = "bartowski/Llama-3.2-3B-Instruct-GGUF",
            filename = "Llama-3.2-3B-Instruct-IQ4_XS.gguf",
            description = "Best size/quality ratio for 3B",
            sizeBytes = 1_830_000_000L,
            quant = "IQ4_XS",
            contextSize = 131072,
            minRamGb = 3
        ),
        AvailableGgufModel(
            id = "llama-3.2-3b-q4-k-m",
            displayName = "Llama 3.2 3B Q4_K_M",
            repo = "bartowski/Llama-3.2-3B-Instruct-GGUF",
            filename = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            description = "Recommended 4-bit for 3B, great quality",
            sizeBytes = 2_020_000_000L,
            quant = "Q4_K_M",
            contextSize = 131072,
            minRamGb = 4
        ),
        AvailableGgufModel(
            id = "llama-3.2-3b-q5-k-m",
            displayName = "Llama 3.2 3B Q5_K_M",
            repo = "bartowski/Llama-3.2-3B-Instruct-GGUF",
            filename = "Llama-3.2-3B-Instruct-Q5_K_M.gguf",
            description = "High quality 5-bit, near-lossless",
            sizeBytes = 2_320_000_000L,
            quant = "Q5_K_M",
            contextSize = 131072,
            minRamGb = 4
        ),
        AvailableGgufModel(
            id = "qwen3-0.6b-q4-0",
            displayName = "Qwen3 0.6B Q4_0",
            repo = "unsloth/Qwen3-0.6B-GGUF",
            filename = "Qwen3-0.6B-Q4_0.gguf",
            description = "Tiny 0.6B model, ~350 MB, fast on any device",
            sizeBytes = 367_000_000L,
            quant = "Q4_0",
            contextSize = 32768,
            minRamGb = 2
        ),
        AvailableGgufModel(
            id = "qwen3-1.7b-q4-0",
            displayName = "Qwen3 1.7B Q4_0",
            repo = "unsloth/Qwen3-1.7B-GGUF",
            filename = "Qwen3-1.7B-Q4_0.gguf",
            description = "Lightweight 1.7B, good quality for size",
            sizeBytes = 996_000_000L,
            quant = "Q4_0",
            contextSize = 131072,
            minRamGb = 3
        ),
        AvailableGgufModel(
            id = "qwen3-8b-q4-k-m",
            displayName = "Qwen3 8B Q4_K_M",
            repo = "unsloth/Qwen3-8B-GGUF",
            filename = "Qwen3-8B-Q4_K_M.gguf",
            description = "Powerful 8B for complex tasks",
            sizeBytes = 4_900_000_000L,
            quant = "Q4_K_M",
            contextSize = 131072,
            minRamGb = 6
        ),
        AvailableGgufModel(
            id = "granite-h-tiny-q4-k-m",
            displayName = "Granite 4.0 H-Tiny Q4_K_M",
            repo = "ibm-granite/granite-4.0-h-tiny-GGUF",
            filename = "granite-4.0-h-tiny-Q4_K_M.gguf",
            description = "IBM Granite 7B hybrid model, Q4_K_M",
            sizeBytes = 4_230_000_000L,
            quant = "Q4_K_M",
            contextSize = 131072,
            minRamGb = 5
        ),
        AvailableGgufModel(
            id = "granite-h-small-q4-k-m",
            displayName = "Granite 4.0 H-Small Q4_K_M",
            repo = "ibm-granite/granite-4.0-h-small-GGUF",
            filename = "granite-4.0-h-small-Q4_K_M.gguf",
            description = "IBM Granite 9.8B hybrid model",
            sizeBytes = 5_900_000_000L,
            quant = "Q4_K_M",
            contextSize = 131072,
            minRamGb = 7
        ),
        AvailableGgufModel(
            id = "lfm-2.5-iq4-xs",
            displayName = "LFM-2.5 1.2B IQ4_XS",
            repo = "LiquidAI/LFM-2.5-1.2B-Instruct-GGUF",
            filename = "LFM-2.5-1.2B-Instruct-IQ4_XS.gguf",
            description = "Liquid AI LFM-2.5, efficient 1.2B (gated)",
            sizeBytes = 900_000_000L,
            quant = "IQ4_XS",
            contextSize = 131072,
            minRamGb = 2,
            isGated = true
        ),
        AvailableGgufModel(
            id = "phi4-mini-q4-k-m",
            displayName = "Phi-4 Mini Q4_K_M",
            repo = "microsoft/Phi-4-mini-instruct-gguf",
            filename = "Phi-4-mini-instruct-Q4_K_M.gguf",
            description = "Microsoft Phi-4 Mini 3.8B, strong coding (gated)",
            sizeBytes = 2_300_000_000L,
            quant = "Q4_K_M",
            contextSize = 131072,
            minRamGb = 4,
            isGated = true
        )
    )
}
