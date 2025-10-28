package vocabulary.app.aspect;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Log4j2
@Aspect
@Component
public class ErrorLoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingAspect.class);

    @Around("execution(* vocabulary.app.strategy.*.*(..))")
    public Object handleSaveError(ProceedingJoinPoint joinPoint){
        try{
            return joinPoint.proceed();
        } catch (IllegalArgumentException e){
            logger.error("데이터 저장 중 유효하지 않은 인자 예외 발생: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        } catch (Throwable e) {
            logger.error("데이터 저장 중 예상치 못한 예외 또는 에러 발생", e);
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        }
    }

}
