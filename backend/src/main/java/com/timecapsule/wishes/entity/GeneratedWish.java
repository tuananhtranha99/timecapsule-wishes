package com.timecapsule.wishes.entity;

import com.timecapsule.wishes.enums.OccasionType;
import com.timecapsule.wishes.enums.WishLanguage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "generated_wishes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedWish {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "occasion_type", length = 50, nullable = false)
    private OccasionType occasionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 10, nullable = false)
    private WishLanguage language;

    @Column(name = "generated_text", columnDefinition = "TEXT", nullable = false)
    private String generatedText;

    @Column(name = "edited_text", columnDefinition = "TEXT")
    private String editedText;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @ManyToMany
    @JoinTable(
            name = "generated_wish_milestones",
            joinColumns = @JoinColumn(name = "wish_id"),
            inverseJoinColumns = @JoinColumn(name = "milestone_id")
    )
    @Builder.Default
    private Set<Milestone> milestones = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
