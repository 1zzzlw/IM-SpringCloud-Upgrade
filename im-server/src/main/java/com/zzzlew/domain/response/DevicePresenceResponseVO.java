package com.zzzlew.domain.response;

import com.zzzlew.domain.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DevicePresenceResponseVO extends Message {
    private boolean desktopOnline;
    private boolean appOnline;

    @Override
    public int getMessageType() {
        return DevicePresenceResponseVO;
    }
}
