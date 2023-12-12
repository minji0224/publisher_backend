# FROM 기반으로 할 이미지
# eclipse-temurin 프로젝트의 JRE 17버전
FROM eclipse-temurin:17-jre
# VOLUME: 컨테이너를 연결할 폴더(디렉터리)
# tmp(temparary) 임시 이미지 디렉터리
#VOLUME /tmp
# ARG: 자바실행 환경변수
ARG JAR_FILE=build/libs/*.jar
# COPY: 호스트운영체재(window)의 실제경로에서 파일을 VOLUME 경로에 복사
COPY ${JAR_FILE} app.jar
# ENTRYPOINT : 컨테이너가 구동될 때 실행하는 명령어
# cmd -> java -jar aaa.jar
# fun main(args: Array<String>) -> 배열로 매개변수에 들어감
ENTRYPOINT ["java", "-jar", "/app.jar"]