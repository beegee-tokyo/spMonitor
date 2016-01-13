package tk.giesecke.spmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** spMonitor - AutoStart
 *
 * Start refresh timer for app widgets (if widgets are placed
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class AutoStart extends BroadcastReceiver {

	public AutoStart() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			// Start service to register background services
			context.startService(new Intent(context, BroadcastRegisterService.class));
		}
	}
}
