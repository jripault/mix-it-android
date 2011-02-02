/*
 * Copyright 2010 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.mixit.android.io;

public abstract class BaseHandler {

    private final String mAuthority;
	private boolean localSync;
	
	public BaseHandler(String authority) {
		this.mAuthority = authority;
		this.localSync = false;
	}

	public String getAuthority() {
		return mAuthority;
	}
	
	public boolean isLocalSync() {
		return localSync;
	}

	public boolean isRemoteSync() {
		return !localSync;
	}

	public void setLocalSync(boolean localSync) {
		this.localSync = localSync;
	}
	
}
