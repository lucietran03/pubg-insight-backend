package com.pubginsight.client.gemini.dto;

import java.util.List;

public record GeminiGenerateContentRequest(List<GeminiContent> contents) {
}
