package org.exoplatform.wallet.model;

import java.util.HashMap;
import java.util.Map;

import groovy.transform.ToString;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ToString
public class WebSocketMessage {

  String              eventId;

  Map<String, Object> message;

  public WebSocketMessage(String eventId, Object... data) {
    this.eventId = eventId;
    if (data != null && data.length > 0) {
      message = new HashMap<>();
      for (Object object : data) {
        if (object != null) {
          message.put(object.getClass().getSimpleName().toLowerCase(), object);
        }
      }
    }
  }

}
