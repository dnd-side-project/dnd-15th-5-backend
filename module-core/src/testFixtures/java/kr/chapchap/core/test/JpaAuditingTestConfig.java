package kr.chapchap.core.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 프로덕션에서는 app-server의 JpaAuditingConfig(@EnableJpaAuditing)가 이 역할을 한다.
// 도메인 모듈의 @DataJpaTest는 app-server 설정을 가져올 수 없어 BaseTimeEntity의
// created_at/updated_at이 채워지지 않으므로, 테스트에서 별도로 활성화해준다.
@EnableJpaAuditing
@TestConfiguration(proxyBeanMethods = false)
public class JpaAuditingTestConfig {
}
