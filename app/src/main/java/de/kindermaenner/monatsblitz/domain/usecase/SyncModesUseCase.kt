package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.ModeRepository

class SyncModesUseCase(
    private val modeRepository: ModeRepository
) {
    suspend operator fun invoke() {
        modeRepository.syncModes()
    }
}
