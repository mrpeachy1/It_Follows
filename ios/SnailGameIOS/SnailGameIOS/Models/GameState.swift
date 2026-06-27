import Foundation

struct GameState {
    var isActive = false
    var elapsedSeconds = 0
    var score = 0
    var player: Coordinate?
    var snail: Coordinate?
    var snailSpeedMetersPerSecond = 0.6
    var gameOverRadiusMeters = 5.0
    var lastTick = Date()

    var distanceToSnailMeters: Double? {
        guard let player, let snail else { return nil }
        return GeoMath.distanceMeters(from: player, to: snail)
    }
}
