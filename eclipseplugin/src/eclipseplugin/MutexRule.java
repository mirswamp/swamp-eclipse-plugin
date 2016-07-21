package eclipseplugin;

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
