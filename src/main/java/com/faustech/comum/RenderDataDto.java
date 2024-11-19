package com.faustech.comum;

import lombok.Builder;

@Builder
public record RenderDataDto(float[] vertex, float[] pixel) {}
