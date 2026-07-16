package com.bubbletea.notification.controller;

import com.bubbletea.notification.exception.dto.NotificationErrorResponseDto;
import com.bubbletea.notification.service.NotificationService;
import com.bubbletea.notification.service.NotificationSseService;
import com.bubbletea.notification.service.dto.NotificationListResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "인앱 알림 API")
public class NotificationController {

  private final NotificationService notificationService;
  private final NotificationSseService notificationSseService;

  @GetMapping("/list")
  @Operation(summary = "알림 목록 조회", description = "수신자의 알림을 최신순으로 조회합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "알림 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = NotificationListResponseDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(schema = @Schema(implementation = NotificationErrorResponseDto.class))
      )
  })
  public NotificationListResponseDto getNotifications(
      @Parameter(name = "receiverId", description = "알림 수신자 ID", example = "1", required = true)
      @RequestParam Long receiverId,
      @Parameter(name = "page", description = "페이지 번호(0부터 시작)", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(name = "size", description = "페이지 크기(최대 100)", example = "20")
      @RequestParam(defaultValue = "20") int size
  ) {
    return notificationService.getNotifications(receiverId, page, size);
  }

  @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(
      summary = "SSE 연결",
      description = "수신자의 실시간 알림 SSE 연결을 생성합니다. 연결 직후 connect 이벤트를 전송하고, "
          + "25초마다 heartbeat comment를 전송합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "SSE 연결 성공",
          content = @Content(
              mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
              schema = @Schema(
                  type = "string",
                  example = "retry: 3000\\nevent: connect\\ndata: {\"receiverId\":1}"
              )
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(schema = @Schema(implementation = NotificationErrorResponseDto.class))
      )
  })
  public SseEmitter connect(
      @Parameter(name = "receiverId", description = "알림 수신자 ID", example = "1", required = true)
      @RequestParam Long receiverId
  ) {
    return notificationSseService.connect(receiverId);
  }

  @PatchMapping("/read/{notificationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "알림 읽음 처리", description = "수신자에게 속한 알림을 읽음 상태로 변경합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(schema = @Schema(implementation = NotificationErrorResponseDto.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "알림을 찾을 수 없음",
          content = @Content(schema = @Schema(implementation = NotificationErrorResponseDto.class))
      )
  })
  public void readNotification(
      @Parameter(name = "notificationId", description = "알림 ID", example = "1", required = true)
      @PathVariable Long notificationId,
      @Parameter(name = "receiverId", description = "알림 수신자 ID", example = "1", required = true)
      @RequestParam Long receiverId
  ) {
    notificationService.readNotification(notificationId, receiverId);
  }
}
