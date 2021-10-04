/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc;

import java.io.IOException;
import java.util.List;

import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;

public class OcClient {

    /* ------------ Methods ------------ */

    public void send(final OcCommand command,
                     final OcContact contact)
    {
        final List<String> cmd = command.build(contact);
        final ProcessBuilder pb = new ProcessBuilder().command(cmd);

        try {
            final Process process = pb.start();
            final int exitValue = process.waitFor();
            if (exitValue != 0) {
                throw new RuntimeException("Unable to execute:" +
                        " command=" + cmd +
                        ", exit_value= " + exitValue);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Execution error: command=" + cmd, e);
        }
    }
}
