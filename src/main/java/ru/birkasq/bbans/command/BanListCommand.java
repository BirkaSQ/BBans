package ru.birkasq.bbans.command;

import ru.birkasq.bbans.BBansPlugin;
import ru.birkasq.bbans.model.PunishmentType;

/** /banlist [страница] */
public class BanListCommand extends ActiveListCommand {

    public BanListCommand(BBansPlugin plugin) {
        super(plugin, "bbans.banlist", PunishmentType.BAN, "banlist");
    }
}
