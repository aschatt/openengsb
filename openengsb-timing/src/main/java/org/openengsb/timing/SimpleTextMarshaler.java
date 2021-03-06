/**

   Copyright 2009 OpenEngSB Division, Vienna University of Technology

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   
 */
package org.openengsb.timing;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.quartz.support.DefaultQuartzMarshaler;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SimpleTextMarshaler extends DefaultQuartzMarshaler {

    @Override
    public void populateNormalizedMessage(final NormalizedMessage message, final JobExecutionContext context)
            throws JobExecutionException, MessagingException {
        super.populateNormalizedMessage(message, context);
        message.setContent(new StringSource((String) context.getJobDetail().getJobDataMap().get("xml")));
    }

}
