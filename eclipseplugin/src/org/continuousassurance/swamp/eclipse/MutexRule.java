/*
 * Copyright 2016-2017 Malcolm Reid Jr.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.continuousassurance.swamp.eclipse;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/* This class prevents SWAMP jobs from running at the same time */
/* For more details, see https://wiki.eclipse.org/FAQ_How_do_I_prevent_two_jobs_from_running_at_the_same_time%3F */
public class MutexRule implements ISchedulingRule {

	@Override
	public boolean contains(ISchedulingRule rule) {
		if (!(rule instanceof MutexRule)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		if (!(rule instanceof MutexRule)) {
			return false;
		}
		return true;
	}
}
