package com.ts.platform.utils.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSLLoggable {

    protected final transient Logger logger
        = LoggerFactory.getLogger(getClass());

    protected Logger getLogger() {
        return logger;
    }
}
