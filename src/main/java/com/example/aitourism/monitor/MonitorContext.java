package com.example.aitourism.monitor;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.io.Serial;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorContext implements Serializable {

    //

    private String userId;

    private String sessionId;

    @Serial
    private static final long serialVersionUID = 1L;
}

