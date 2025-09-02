package com.company.feedbackbot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity @Table(name="feedbacks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Feedback {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private StaffProfile staffProfile;

  @Column(columnDefinition="text", nullable=false)
  private String text;

  @Enumerated(EnumType.STRING)
  private Sentiment sentiment;

  private Integer criticality;

  @Column(columnDefinition="text")
  private String resolution;

  @CreationTimestamp
  private Instant createdAt;
}
