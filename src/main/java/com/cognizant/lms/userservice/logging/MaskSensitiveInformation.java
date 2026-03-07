package com.cognizant.lms.userservice.logging;


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin( name = "MaskSensitive", category = PatternConverter.CATEGORY )
@ConverterKeys( { "maskSensitive" } )
public class MaskSensitiveInformation extends LogEventPatternConverter {

    protected MaskSensitiveInformation(String name, String style) {
        super(name,style);
    }

    @PluginFactory
    public static MaskSensitiveInformation newInstance() {
        return new MaskSensitiveInformation("maskSensitive","maskSensitive");
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String message = event.getMessage().getFormattedMessage();
        String maskedMessage = maskData(message);
        toAppendTo.append(maskedMessage);
    }

    private String maskData(String message) {
        if (message == null)
            return null;
       message = message.replaceAll("(?i)(password[\\s:=]+)([^\\s,]*)","$1*****");
       message = maskEmail(message);

        return message;
    }

    private String maskEmail(String message) {
        Pattern pattern = Pattern.compile("([\\w.]+)@(\\w+)\\.(\\w+)");
        Matcher matcher = pattern.matcher(message);
        StringBuffer maskedMessage = new StringBuffer();
        if (matcher.find()){
            String userName = matcher.group(1);
            String domain = matcher.group(2);
            String tld  = matcher.group(3);
            String maskedUser;
            if (userName.length() < 5) {
                maskedUser = userName.charAt(0)
                        + "*".repeat(userName.length() - 2)
                        + userName.charAt(userName.length() - 1);
            } else {
                maskedUser = userName.substring(0,2)
                        + "*".repeat(userName.length()-4)
                        + userName.substring(userName.length()-2);
            }
            String maskDomain = domain.charAt(0)
                    + "*".repeat(domain.length()-2)
                    + domain.charAt(domain.length()-1);
            String maskedEmail =  maskedUser + "*" + maskDomain + "*" +tld;
            matcher.appendReplacement(maskedMessage, maskedEmail);
        }
        matcher.appendTail(maskedMessage);
        return maskedMessage.toString();
    }
}
