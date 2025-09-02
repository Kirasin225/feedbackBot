package com.company.feedbackbot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity @Table(name="staff_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffProfile {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, unique=true)
  private Long telegramUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private Role role;

  @Column(nullable=false)
  private String branch;

  @CreationTimestamp
  private Instant createdAt;
}
