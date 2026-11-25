package entity.board;

import entity.Player;
import entity.card.CharacterCard;
import entity.card.SpellCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regression coverage for the same-tile transfer that previously deleted its own contents. */
class BoardSameTileRegressionTest {
    @Test
    void sameTileRequestMustPreserveCharacterAndAttachedSpells() {
        Board board = new Board();
        Player player = new Player("Player 1");
        CharacterCard character = new CharacterCard(player, "Aki", 200, 3);
        SpellCard spell = new SpellCard(player, "Fireball");
        SpellCard secondSpell = new SpellCard(player, "Magic Missiles");
        board.playCard(1, 2, character);
        board.playCard(1, 2, spell);
        board.playCard(1, 2, secondSpell);
        Tile originalTile = board.getTile(1, 2);
        var attachedSpells = board.getTile(1, 2).getSpells();

        IllegalMoveException exception = assertThrows(IllegalMoveException.class,
                () -> board.moveTile(1, 2, 1, 2));

        assertAll(
                () -> assertEquals("Source and destination must be different tiles.", exception.getMessage()),
                () -> assertSame(originalTile, board.getTile(1, 2)),
                () -> assertSame(character, originalTile.getCharacterCard()),
                () -> assertSame(attachedSpells, originalTile.getSpells()),
                () -> assertEquals(List.of(spell, secondSpell), originalTile.getSpells()),
                () -> assertFalse(originalTile.isEmpty()),
                () -> assertNull(originalTile.getSorceryCard())
        );
    }
}
