package com.love.yourvoiceback.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, unique = true, nullable = false)
    private String nickName;

    @Column(length = 255, unique = true, nullable = false)
    private String email;

    @Column(length = 255)
    private String password;

    public static User of(String nickName, String email, String password){
        return User.builder()
                .nickName(nickName)
                .email(email)
                .password(password)
                .build();
    }
}
