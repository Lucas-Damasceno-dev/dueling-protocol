// Helper script to map blockchain addresses to player usernames
// This would need to connect to PostgreSQL to get real usernames
// For now, we return a mapping based on hash of player ID

const playerNameMap = {
    "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266": "Player1",
    "0x70997970C51812dc3A010C7d01b50e0d17dc79C8": "Player2", 
    "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC": "Player3",
    "0x90F79bf6EB2c4f870365E785982E1f101E93b906": "Player4",
    "0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65": "Player5",
    "0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc": "Player6",
    "0x976EA74026E726554dB657fA54763abd0C3a0aa9": "Player7"
};

function getPlayerName(address) {
    return playerNameMap[address] || `Player (${address.substring(0, 10)}...)`;
}

module.exports = { getPlayerName, playerNameMap };
