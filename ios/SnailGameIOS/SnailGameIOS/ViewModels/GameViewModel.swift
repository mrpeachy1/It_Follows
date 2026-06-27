import Foundation
import MapKit

@MainActor
final class GameViewModel: ObservableObject {
    @Published private(set) var state = GameState()
    @Published var cameraPosition: MapCameraPosition = .automatic

    private var timer: Timer?

    func updatePlayer(_ player: Coordinate?) {
        guard let player else { return }
        state.player = player
        if state.snail == nil {
            state.snail = GeoMath.moveToward(
                from: Coordinate(latitude: player.latitude, longitude: player.longitude - 0.0005),
                to: player,
                distanceMeters: 0
            )
        }
        cameraPosition = .region(MKCoordinateRegion(center: player.clLocationCoordinate, span: MKCoordinateSpan(latitudeDelta: 0.003, longitudeDelta: 0.003)))
    }

    func startGame() {
        state.isActive = true
        state.lastTick = Date()
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
    }

    func stopGame() {
        state.isActive = false
        timer?.invalidate()
        timer = nil
    }

    func resetGame() {
        let player = state.player
        state = GameState(player: player)
        if let player { updatePlayer(player) }
    }

    func tick(now: Date = Date()) {
        guard state.isActive, let player = state.player, let snail = state.snail else { return }
        let delta = max(0, now.timeIntervalSince(state.lastTick))
        state.lastTick = now
        state.elapsedSeconds += Int(delta.rounded())
        let step = state.snailSpeedMetersPerSecond * delta
        state.snail = GeoMath.moveToward(from: snail, to: player, distanceMeters: step)
        if GeoMath.isWithinProximity(state.snail ?? snail, player, radiusMeters: state.gameOverRadiusMeters) {
            stopGame()
        } else {
            state.score += max(1, Int(delta))
        }
    }
}
