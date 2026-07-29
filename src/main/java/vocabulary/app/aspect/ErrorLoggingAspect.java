package vocabulary.app.aspect;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Log4j2
@Aspect
@Component
public class ErrorLoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingAspect.class);

    // strategy 계층의 예외를 "기록"만 한다.
    //
    // 예전에는 여기서 ResponseEntity를 반환했는데, 대상 메서드의 반환형은 Word / List<Word> / WordFolder 라서
    // 호출부(WordService.save 등)에서 ClassCastException이 터졌다.
    // 결과적으로 "pronunciation 누락(400)"이 정체불명의 500으로 둔갑했다.
    // 예외는 그대로 흘려보내고 HTTP 상태 변환은 GlobalExceptionHandler 한 곳에서 담당한다.
    @Around("execution(* vocabulary.app.strategy.*.*(..))")
    public Object handleSaveError(ProceedingJoinPoint joinPoint) throws Throwable {
        try{
            return joinPoint.proceed();
        } catch (IllegalArgumentException e){
            logger.error("데이터 저장 중 유효하지 않은 인자 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Throwable e) {
            logger.error("데이터 저장 중 예상치 못한 예외 또는 에러 발생", e);
            throw e;
        }
    }

}
