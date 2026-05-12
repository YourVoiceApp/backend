package com.love.yourvoiceback.room.controller.dto.response;

import java.util.List;

public record RoomBrowsePageResponse(
        List<RoomBrowseResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
}
