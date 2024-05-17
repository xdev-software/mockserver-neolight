/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.mockserver.serialization.model;

import java.util.Objects;

public class WebSocketClientIdDTO {

    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public WebSocketClientIdDTO setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final WebSocketClientIdDTO that))
        {
            return false;
        }
        if(!super.equals(o))
        {
            return false;
        }
		return Objects.equals(getClientId(), that.getClientId());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), getClientId());
    }
}
