package com.love.yourvoiceback.market;

import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.domain.VoiceAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "voice_license")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false, unique = true)
    private VoicePurchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voice_asset_id", nullable = false)
    private VoiceAsset voiceAsset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_user_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LicenseScope scope;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    public enum LicenseScope {
        PERSONAL_USE,
        PREVIEW_ONLY,
        COMMERCIAL_USE
    }
}
