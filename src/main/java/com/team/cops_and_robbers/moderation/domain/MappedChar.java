package com.team.cops_and_robbers.moderation.domain;

record MappedChar(int codePoint, int sourceStart, int sourceEnd) {

    MappedChar withCodePoint(int newCodePoint) {
        return new MappedChar(newCodePoint, sourceStart, sourceEnd);
    }
}
