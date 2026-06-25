package model.Configuration;

public class ConfigManager {
    public GameConfigBuilder getConfig(String configName) {
        if ("default-determinized".equals(configName)) {
            return defaultConfig().setDeterminized(true);
        }
        return defaultConfig();
    }

    public GameConfigBuilder defaultConfig() {
        return new GameConfigBuilder()
                .setPlayerCount(2)
                .setRandomSeed(100)
                .addTile().setGraphicsId(0).setOccurrences(2).markAsIsMonastery().bottom().markAsRoadAndAsEnd().endTile()
                .addTile().setGraphicsId(1).setOccurrences(4).markAsIsMonastery().endTile()
                .addTile().setGraphicsId(2).setOccurrences(1).markAsHasBlazon().top().markAsCastle().markAsConnectCastles().bottom().markAsCastle().left().markAsCastle().right().markAsCastle().endTile()
                .addTile().setGraphicsId(3).setOccurrences(3).right().markAsCastle().top().markAsRoadAndConnectBottom().endTile()
                .addTile().setGraphicsId(4).setOccurrences(5).top().markAsCastle().endTile()
                .addTile().setGraphicsId(5).setOccurrences(2).markAsConnectCastles().markAsHasBlazon().left().markAsCastle().right().markAsCastle().endTile()
                .addTile().setGraphicsId(6).setOccurrences(1).markAsConnectCastles().top().markAsCastle().bottom().markAsCastle().endTile()
                .addTile().setGraphicsId(7).setOccurrences(3).left().markAsCastle().right().markAsCastle().endTile()
                .addTile().setGraphicsId(8).setOccurrences(2).bottom().markAsCastle().right().markAsCastle().endTile()
                .addTile().setGraphicsId(9).setOccurrences(3).top().markAsCastle().bottom().markAsRoadAndConnectRight().endTile()
                .addTile().setGraphicsId(10).setOccurrences(3).left().markAsRoadAndConnectTop().right().markAsCastle().endTile()
                .addTile().setGraphicsId(11).setOccurrences(3).left().markAsRoadAndAsEnd().top().markAsRoadAndAsEnd().bottom().markAsRoadAndAsEnd().right().markAsCastle().endTile()
                .addTile().setGraphicsId(12).setOccurrences(2).markAsConnectCastles().markAsHasBlazon().top().markAsCastle().left().markAsCastle().endTile()
                .addTile().setGraphicsId(13).setOccurrences(3).markAsConnectCastles().left().markAsCastle().top().markAsCastle().endTile()
                .addTile().setGraphicsId(14).setOccurrences(2).markAsConnectCastles().markAsHasBlazon().top().markAsCastle().left().markAsCastle().bottom().markAsRoadAndConnectRight().endTile()
                .addTile().setGraphicsId(15).setOccurrences(3).markAsConnectCastles().top().markAsCastle().left().markAsCastle().bottom().markAsRoadAndConnectRight().endTile()
                .addTile().setGraphicsId(16).setOccurrences(1).markAsHasBlazon().markAsConnectCastles().left().markAsCastle().top().markAsCastle().right().markAsCastle().endTile()
                .addTile().setGraphicsId(17).setOccurrences(3).markAsConnectCastles().left().markAsCastle().top().markAsCastle().right().markAsCastle().endTile()
                .addTile().setGraphicsId(18).setOccurrences(2).markAsHasBlazon().markAsConnectCastles().left().markAsCastle().top().markAsCastle().right().markAsCastle().bottom().markAsRoadAndAsEnd().endTile()
                .addTile().setGraphicsId(19).setOccurrences(1).markAsConnectCastles().left().markAsCastle().top().markAsCastle().right().markAsCastle().bottom().markAsRoadAndAsEnd().endTile()
                .addTile().setGraphicsId(20).setOccurrences(8).bottom().markAsRoadAndConnectTop().endTile()
                .addTile().setGraphicsId(21).setOccurrences(9).bottom().markAsRoadAndConnectLeft().endTile()
                .addTile().setGraphicsId(22).setOccurrences(4).left().markAsRoadAndAsEnd().right().markAsRoadAndAsEnd().bottom().markAsRoadAndAsEnd().endTile()
                .addTile().setGraphicsId(23).setOccurrences(1).left().markAsRoadAndAsEnd().right().markAsRoadAndAsEnd().bottom().markAsRoadAndAsEnd().top().markAsRoadAndAsEnd().endTile()
                .setStartField().setGraphicsId(3).setOccurrences(1).right().markAsCastle().top().markAsRoadAndConnectBottom().endTile();
    }
}
