package entity.card;

import entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterCardTest {
    private final CharacterCard character = new CharacterCard(new Player("Player 1"), "Aki", 200, 3);

    @Test
    void newCharacterCanMoveThenCastAndResetBothActions() {
        assertFalse(character.hasMovedThisTurn());
        assertFalse(character.hasCastThisTurn());
        character.recordMove();
        character.recordCast();
        assertTrue(character.hasMovedThisTurn());
        assertTrue(character.hasCastThisTurn());
        character.resetTurnActions();
        assertFalse(character.hasMovedThisTurn());
        assertFalse(character.hasCastThisTurn());
        character.recordMove();
        assertTrue(character.hasMovedThisTurn());
    }

    @Test
    void cannotRecordASecondMove() {
        character.recordMove();
        assertThrows(IllegalStateException.class, character::recordMove);
        assertTrue(character.hasMovedThisTurn());
        assertFalse(character.hasCastThisTurn());
    }

    @Test
    void castingPreventsLaterMovementWithoutInventingACastCountLimit() {
        character.recordCast();
        character.recordCast();
        assertThrows(IllegalStateException.class, character::recordMove);
        assertFalse(character.hasMovedThisTurn());
        assertTrue(character.hasCastThisTurn());
    }

    @Test
    void actionResetDoesNotEraseKoHistoryPendingRevivalRules() {
        character.takeDamage(200);
        character.revive();
        character.resetTurnActions();
        assertTrue(character.hasBeenKnockedOut());
        assertEquals(200, character.getHP());
    }

    @Test
    void clampsDamageAndHealingToHealthBounds() {
        character.takeDamage(50);
        assertEquals(150, character.getHP());

        character.takeDamage(300);
        assertEquals(0, character.getHP());

        character.takeDamage(-300);
        assertEquals(200, character.getHP());
    }

    @Test
    void reviveRestoresFullHealthOnlyAfterKnockout() {
        character.takeDamage(50);
        character.revive();
        assertEquals(150, character.getHP());

        character.takeDamage(150);
        character.revive();
        assertEquals(200, character.getHP());
    }
}
