package com.faustech.dto;

import lombok.Builder;

@Builder
public record RenderDataDto(float[] vertex, float[] pixel) {}
