/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.enums;

/**
 * We do not really use it. See {@link AlertType}.
 *
 * TODO: Remove and cleanup dependencies.
 */
@Deprecated
public enum MetricAlertType {

    SINGLE_METRIC((byte)0),COMPOSITE((byte)1),PERIOD_OVER_PERIOD((byte)2);

    private byte b;

    private MetricAlertType(byte b) {
        this.b = b;
    }

    public byte getId() {
        return b;
    }

    public MetricAlertType getExecutorTypeById(byte b) {
        switch (b) {
            case 0:
                return SINGLE_METRIC;
        }

        throw new AssertionError("Bad AlertExectorType: " + b);
    }

}
