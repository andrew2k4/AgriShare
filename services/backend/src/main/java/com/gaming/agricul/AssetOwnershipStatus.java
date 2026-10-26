package com.gaming.agricul;

/**
 * The two models the document distinguishes (§9.1): an asset is either contributed to the
 * project's capital (and the contributor no longer owns it, see IN_KIND contributions), or
 * it stays owned by the contributor and is merely leased to the project for rent.
 */
public enum AssetOwnershipStatus {
    OWNED_BY_CONTRIBUTOR,
    TRANSFERRED_TO_PROJECT
}
