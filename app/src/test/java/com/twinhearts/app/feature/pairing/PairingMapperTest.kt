package com.twinhearts.app.feature.pairing

import com.twinhearts.app.feature.pairing.data.mapper.toDomain
import com.twinhearts.app.feature.pairing.data.remote.StatusDto
import com.twinhearts.app.feature.pairing.domain.model.PairingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PairingMapperTest {
    @Test fun connected() = assertEquals(PairingStatus.Connected("Ravi"), StatusDto("connected", partnerName = "Ravi").toDomain())
    @Test fun connectedWithoutName_usesPartner() = assertEquals(PairingStatus.Connected("Partner"), StatusDto("connected").toDomain())
    @Test fun waiting() = assertEquals(
        PairingStatus.Waiting("r1", "Asha", "123", "2026-10-25T10:00:00Z"),
        StatusDto("waiting", requestId = "r1", receiverName = "Asha", receiverPhone = "123", createdAt = "2026-10-25T10:00:00Z").toDomain(),
    )
    @Test fun invitationReceived() = assertEquals(PairingStatus.InvitationReceived("Asha"), StatusDto("invitation_received", senderName = "Asha").toDomain())
    @Test fun unknownIsNotConnected() = assertEquals(PairingStatus.NotConnected, StatusDto("something_else").toDomain())
}
