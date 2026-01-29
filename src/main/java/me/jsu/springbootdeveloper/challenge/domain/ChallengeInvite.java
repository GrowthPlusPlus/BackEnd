package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.enumType.InviteStatus;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "challenge_invite")
public class ChallengeInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter; // 초대한 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee; // 초대받은 사람

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InviteStatus status; // INVITED, ACCEPTED, REJECTED 등

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    //챌린지, 초대하는 사람, 초대되는 사람
    @Builder
    public ChallengeInvite(Challenge challenge, User inviter, User invitee) {
        this.challenge = challenge;
        this.inviter = inviter;
        this.invitee = invitee;
        this.status = InviteStatus.INVITED; // 기본 상태
    }

    public void updateStatus(InviteStatus status) {
        this.status = status;
    }
}
