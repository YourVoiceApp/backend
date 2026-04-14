package com.love.yourvoiceback.room.service;

import com.love.yourvoiceback.room.reopository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService {

    private RoomRepository roomRepository;

}
