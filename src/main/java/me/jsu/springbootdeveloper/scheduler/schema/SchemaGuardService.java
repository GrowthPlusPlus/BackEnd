// 정성우

package me.jsu.springbootdeveloper.scheduler.schema;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
    DB에 새로운 칼럼이 추가되었을 때, 스케줄러가 업데이트 되지 않은 DB 칼럼을 참조하는 것을 방지하기 위해 사용한다.
 */
@Service
@RequiredArgsConstructor
public class SchemaGuardService {
    private final DataSource dataSource;

    /*
        user_statistics 테이블에 특정 컬럼이 존재하는지 확인한다.
     */
    public boolean isUserStatisticsSchemaReady() {
        String sql = """
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'user_statistics'
              AND column_name = 'most_failed_updated_at'
        """;

        // Connection, Connection, ResultSet 들을 자동으로 닫아준다.
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next(); // 칼럼을 조회한 결과가 1개 이상 있으면 true를 반환한다.

        } catch (SQLException e) {  // 스키마가 준비가 안된 경우이다.
            // DB 연결 자체가 불안정한 경우도 대비한다.
            return false;
        }
    }
}
