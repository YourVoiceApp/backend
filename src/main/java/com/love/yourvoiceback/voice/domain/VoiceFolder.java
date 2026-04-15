package com.love.yourvoiceback.voice.domain;

import com.love.yourvoiceback.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "voice_folder",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_voice_folder_owner_parent_name", columnNames = {"owner_user_id", "parent_folder_id", "name"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private VoiceFolder parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent")
    private List<VoiceFolder> children = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static VoiceFolder of(User user, VoiceFolder parentFolder, String folderName) {
        VoiceFolder voiceFolder = VoiceFolder.builder()
                .owner(user)
                .name(folderName)
                .build();
        voiceFolder.changeParent(parentFolder);
        return voiceFolder;
    }

    public void changeParent(VoiceFolder parentFolder) {
        if (this.parent != null) {
            this.parent.getChildren().remove(this);
        }

        this.parent = parentFolder;

        if (parentFolder != null && !parentFolder.getChildren().contains(this)) {
            parentFolder.getChildren().add(this);
        }
    }

    public void updateFolder(String folderName, VoiceFolder parentFolder) {
        changeParent(parentFolder);
        this.name = folderName;
        this.updatedAt = LocalDateTime.now();
    }
}
