package ru.birkasq.bbans.command;

import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.PunishmentType;

/** /mutelist [страница] */
public class MuteListCommand extends ActiveListCommand {

    public MuteListCommand(BBansPlugin plugin) {
        super(plugin, "bbans.mutelist", PunishmentType.MUTE, "mutelist");
    }
}
