package com.bubbletea.notification.service;

import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateRenderer {

  private static final Pattern VARIABLE_PATTERN =
      Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*}}");

  public String render(String template, Map<String, Object> variables) {
    Map<String, Object> renderVariables = variables == null ? Map.of() : variables;
    Matcher matcher = VARIABLE_PATTERN.matcher(template);
    StringBuilder renderedTemplate = new StringBuilder();

    while (matcher.find()) {
      String variableName = matcher.group(1);
      if (!renderVariables.containsKey(variableName) || renderVariables.get(variableName) == null) {
        throw new NotificationException(
            NotificationErrorCode.NOTIFICATION_TEMPLATE_VARIABLE_NOT_FOUND
        );
      }

      matcher.appendReplacement(
          renderedTemplate,
          Matcher.quoteReplacement(String.valueOf(renderVariables.get(variableName)))
      );
    }
    matcher.appendTail(renderedTemplate);

    return renderedTemplate.toString();
  }
}
