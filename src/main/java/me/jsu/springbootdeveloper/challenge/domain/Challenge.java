// 정성우

package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.enumType.ChallengeVisibility;
import me.jsu.springbootdeveloper.post.domain.PostPictureTemporary;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/*
    DB에 저장될 챌린지 정보이다.
 */
@Table(
        name = "challenge",
        indexes = {
                @Index(  // 피드를 위해 공개 범위와 시작 일로 최신순으로 인덱싱 한다.
                        name = "idx_visibility_start_date",
                        columnList = "challenge_visibility, start_date"
                ),
                @Index(  // 내가 만든 챌린지를 관리하기 위해 내 id로 인덱싱 한다.
                        name = "idx_user_created",
                        columnList = "user_id"
                ),
                /*
                    챌린지 시작일과 종료일로 인덱싱 하여 진행중, 완료, 종료 상태의 챌린지를 쉽게 찾아낸다.
                    참여자는 챌린지 참여자 DB에서 상태를 알아낼 수 있으므로, 챌린지에 참여하지 않은 유저를 위한 인덱싱이다.
                 */

                @Index(
                        name = "idx_date_range",
                        columnList = "start_date, end_date"
                ),
                @Index(
                        name = "idx_end_date",
                        columnList = "end_date"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    // user DB의 값을 갖는 fk값으로, 챌린지를 생성한 유저의 id를 나타낸다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "max_participant_number", nullable = false)
    private int maxParticipantNumber;

    // 현재 참여하고 있는 참가자 수이다. 파생 데이터지만 캐싱을 위해 사용한다
    @Column(name = "participant_number", nullable = false)
    private int participantNumber;

    @Enumerated(EnumType.STRING)
    private ChallengeVisibility challengeVisibility;  // 챌린지 공개 범위를 설정하기 위한 값이다.

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;  // 챌린지 시작일이다.

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;  // 챌린지 종료일이다.

    @Column(name = "requiredWeeklyCount", nullable = false)
    private int requiredWeeklyCount;  // 주당 최소 인증 횟수이다.

    @Column(name = "photo_required", nullable = false)
    private boolean photoRequired;  // 사진 여부가 필수인지 정하는 값이다.

    @Column(name = "today_success_count", nullable = false)
    private int todaySuccessCount;  // 오늘 해당 챌린지를 달성한 사람이 몇 명인지 기록하는 값이다.

    @Column(name = "today_date", nullable = false)
    private LocalDate todayDate;  // 해당 챌린지의 "오늘"의 기준을 만드는 값이다.

    @Column(name = "active", nullable = false)
    private boolean active;  // 챌린지가 현재 진행중인지 알려주는 값이다. 챌린지 내의 모든 사람이 실패해서 진행될 수 없는 경우, 삭제 대신 검색되지 않도록 한다.

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /*
        다음 코드는 CASCADE 설정으로 Challenge 엔티티를 fk값으로 가지고 있는 DB들을 자동으로 삭제해주는 설정이다.
        챌린지 삭제 시, 밑의 정보들도 함께 전부 삭제된다. (챌린지 참여자, 챌린지 초대 링크, 챌린지 주간 상태, 챌린지 태그)
     */
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.REMOVE)
    private List<ChallengeParticipant> challengeParticipants;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.REMOVE)
    private List<ChallengeInviteLink> challengeInviteLinks;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.REMOVE)
    private List<ChallengeInvite> challengeInvites;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.REMOVE)
    private List<ChallengeWeeklyStatus> challengeWeeklyStatuses;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.REMOVE)
    private List<TagToChallenge> tagMappings;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.REMOVE)
    private List<PostPictureTemporary> postPictureTemporaries;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.REMOVE)
    private List<ChallengeCalenderShare> challengeCalenderShares;

    @Builder
    public Challenge(String title, String content, User user, int maxParticipantNumber, ChallengeVisibility challengeVisibility,
                     LocalDate startDate, LocalDate endDate, int requiredWeeklyCount, boolean photoRequired, LocalDate todayDate) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.maxParticipantNumber = maxParticipantNumber;
        this.participantNumber = 0;
        this.challengeVisibility = challengeVisibility;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requiredWeeklyCount = requiredWeeklyCount;
        this.photoRequired = photoRequired;
        this.todaySuccessCount = 0;
        this.todayDate = LocalDate.now(ZoneId.of("Asia/Seoul"));  // 아시아 시간으로 초기화 한다.
        this.active = true;
    }
    /*
        챌린지 참여 시 인원 초과인지 먼저 확인한다. 최대 50명이 기본값이다.
        반환 값: 인원 초과 시 True, 참여 가능 시 False
     */
    public boolean checkIfOverMaxParticipantNumber() {
        return participantNumber >= maxParticipantNumber;
    }
    /*
        챌린지 설을 변경하는 함수이다. 챌린지 이름, 챌린지 시작일을 제외하고 수정이 가능하다.
     */
    public void updateSettings(LocalDate endDate, int requiredWeeklyCount, String content, Boolean photoRequired, ChallengeVisibility challengeVisibility) {
        this.endDate = endDate;
        this.requiredWeeklyCount = requiredWeeklyCount;
        this.content = content;
        this.photoRequired =  photoRequired;
        this.challengeVisibility = challengeVisibility;
    }

    /*
        챌린지의 오너를 위임하는 함수이다.
     */
    public void updateUser(User user) {
        this.user = user;
    }

    /*
        챌린지에 참여자 수가 늘 때마다 참여자 필드 값을 업데이트 한다.
     */
    public void updateParticipate() {
        this.participantNumber++;
    }

    /*
        챌린지에 참여자 수가 줄 때마다 참여자 필드 값을 업데이트 한다.
     */
    public void updateLeave() {
        this.participantNumber--;
    }

    /*
        참여자 수의 정합성이 깨진 경우, 다시 계산한 값으로 업데이트한다.
     */
    public void synchronizeParticipantNumber(int participantNumber) {
        this.participantNumber = participantNumber;
    }

    /*
        해당 챌린지를 오늘 누군가가 수행할 때마다 하나씩 증가한다.
     */
    public void increaseTodaySuccess(LocalDate today) {
        if (!today.equals(this.todayDate)) {  // 날짜가 같지 않으면 다음 날이므로 날짜를 오늘로 초기화 한다. 방어 코드이다.
            this.todayDate = today;
            this.todaySuccessCount = 0;
        }
        this.todaySuccessCount++;
    }

    /*
        이미 수행한 챌린지를 누군가가 인증글을 삭제하여 취소하는 경우 인증 총 횟수가 감소한다.
     */
    public void decreaseTodaySuccess() {
        this.todaySuccessCount--;
    }

    /*
        서버가 12시마다 모든 챌린지의 날짜를 초기화 하는 함수이다.
        서비스에서 현재 진행중인 챌린지만 찾아서 초기화 한다.
     */
    public void syncToday(LocalDate today) {
        if (!today.equals(this.todayDate)) {
            this.todayDate = today;
            this.todaySuccessCount = 0;
        }
    }

    /**
     * 챌린지 시작일로부터 오늘까지 몇 일이 지났는지 계산하여 반환하는 함수이다.
     * 예: 시작일이 1일이고 오늘이 3일이면 '2'를 반환한다.
     */
    public long calculateDaysSinceStart() {
        // 1. 현재 날짜를 가져온다. (시스템의 기본 시간대 기준)
        LocalDate today = LocalDate.now();

        // 2. 만약 오늘이 시작일보다 이전이라면 (아직 시작 안 함) 0을 반환한다.
        if (today.isBefore(this.startDate)) {
            return 0;
        }

        // 3. 시작일(startDate)과 오늘(today) 사이의 전체 일수 차이를 계산하여 반환한다.
        // 공식: $Days = Today - StartDate$
        return ChronoUnit.DAYS.between(this.startDate, today);
    }

    /**
     * 현재가 챌린지 몇 일차인지 계산하여 반환하는 함수이다. (시작일을 1일차로 계산)
     * 예: 시작일이 1일이고 오늘이 1일이면 '1'을 반환한다.
     */
    public long getChallengeDayCount() {
        // 지난 날짜에 +1을 하면 현재 몇 일차인지 알 수 있다.
        return calculateDaysSinceStart() + 1;
    }

    /*
        챌린지가 조기 실패한 경우, 진행 상태를 false로 바꾸는 함수이다.
     */
    public void failChallenge() {
        this.active = false;
    }
}
