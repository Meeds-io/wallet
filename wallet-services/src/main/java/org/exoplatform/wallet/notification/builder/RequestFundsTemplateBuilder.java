package org.exoplatform.wallet.notification.builder;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.api.notification.template.Element;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.webui.utils.TimeConvertUtils;

public class RequestFundsTemplateBuilder extends AbstractTemplateBuilder {

  private static final Log LOG = ExoLogger.getLogger(RequestFundsTemplateBuilder.class);

  private TemplateProvider templateProvider;

  private boolean          pushNotification;

  private ExoContainer     container;

  public RequestFundsTemplateBuilder(TemplateProvider templateProvider, PortalContainer container, boolean pushNotification) {
    this.templateProvider = templateProvider;
    this.container = container;
    this.pushNotification = pushNotification;
  }

  @Override
  protected MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    String pluginId = notification.getKey().getId();

    String language = getLanguage(notification);
    TemplateContext templateContext =
                                    TemplateContext.newChannelInstance(this.templateProvider.getChannelKey(), pluginId, language);

    String amount = notification.getValueOwnerParameter(AMOUNT);
    amount = formatNumber(amount, language);
    String type = notification.getValueOwnerParameter(ACCOUNT_TYPE);
    String avatar = notification.getValueOwnerParameter(AVATAR);
    String receiver = notification.getValueOwnerParameter(RECEIVER);
    String sender = notification.getValueOwnerParameter(SENDER);
    String receiverUrl = notification.getValueOwnerParameter(RECEIVER_URL);
    String senderUrl = notification.getValueOwnerParameter(SENDER_URL);
    String fundsAcceptURL = notification.getValueOwnerParameter(FUNDS_ACCEPT_URL);
    String user = notification.getValueOwnerParameter(USER);
    String userUrl = notification.getValueOwnerParameter(USER_URL);
    String symbol = notification.getValueOwnerParameter(SYMBOL);
    String message = notification.getValueOwnerParameter(MESSAGE);
    String notificationRead = notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey());
    String fundRequestSent = notification.getValueOwnerParameter(FUNDS_REQUEST_SENT);

    RequestLifeCycle.begin(container);
    try {
      templateContext.put("AMOUNT", amount);
      templateContext.put("ACCOUNT_TYPE", type);
      templateContext.put("SENDER", sender);
      templateContext.put("RECEIVER", receiver);
      templateContext.put("SENDER_URL", senderUrl);
      templateContext.put("RECEIVER_URL", receiverUrl);
      templateContext.put("FUNDS_ACCEPT_URL", fundsAcceptURL);
      templateContext.put("USER", user);
      templateContext.put("USER_URL", userUrl);
      templateContext.put("SYMBOL", symbol);
      templateContext.put("MESSAGE", message);
      templateContext.put("AVATAR", avatar != null ? avatar : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("READ", Boolean.valueOf(notificationRead) ? "read" : "unread");
      templateContext.put("FUNDS_REQUEST_SENT", Boolean.valueOf(fundRequestSent));
      setLastModifiedDate(notification, language, templateContext);

      String body = TemplateUtils.processGroovy(templateContext);
      // binding the exception throws by processing template
      if (templateContext.getException() != null) {
        throw new IllegalStateException("An error occurred while building message", templateContext.getException());
      }
      MessageInfo messageInfo = new MessageInfo();
      messageInfo.to(notification.getTo());
      messageInfo.from(notification.getFrom());
      messageInfo.pluginId(pluginId);
      if (this.pushNotification) {
        messageInfo.subject(fundsAcceptURL);
      } else {
        addMessageSubject(messageInfo, templateContext, type);
      }
      return messageInfo.body(body).end();
    } catch (Exception e) {
      // Exception gets swallowed by upper services, thus log it here
      LOG.warn("An error occurred while building notification message", e);
      throw e;
    } finally {
      RequestLifeCycle.end();
    }
  }

  protected void setLastModifiedDate(NotificationInfo notification, String language, TemplateContext templateContext) {
    try {
      templateContext.put("LAST_UPDATED_TIME", getLastModifiedDate(notification, language));
    } catch (Exception e) {
      templateContext.put("LAST_UPDATED_TIME", "");
    }
  }

  @Override
  protected boolean makeDigest(NotificationContext ctx, Writer writer) {
    return false;
  }

  private void addMessageSubject(MessageInfo messageInfo, TemplateContext templateContext, String type) {
    String pluginId = templateContext.getPluginId();
    PluginConfig templateConfig = getPluginConfig(pluginId);
    pluginId = WalletType.isSpace(type) ? "Space" + pluginId : pluginId;
    Element subjectElement = NotificationUtils.getSubject(templateConfig, pluginId, templateContext.getLanguage());
    if (subjectElement != null && subjectElement.getTemplate() != null) {
      messageInfo.subject(subjectElement.getTemplate());
    }
  }

  private String getLastModifiedDate(NotificationInfo notification, String language) {
    Calendar lastModified = Calendar.getInstance();
    lastModified.setTimeInMillis(notification.getLastModifiedDate());
    return TimeConvertUtils.convertXTimeAgoByTimeServer(lastModified.getTime(),
                                                        "EE, dd yyyy",
                                                        new Locale(language),
                                                        TimeConvertUtils.YEAR);
  }

  private static PluginConfig getPluginConfig(String pluginId) {
    PluginConfig pluginConfig = NotificationContextImpl.cloneInstance().getPluginSettingService().getPluginConfig(pluginId);

    if (pluginConfig == null) {
      throw new IllegalStateException("PluginConfig is NULL with plugId = " + pluginId);
    }

    return pluginConfig;
  }

}
