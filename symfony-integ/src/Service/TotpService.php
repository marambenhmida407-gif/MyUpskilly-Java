<?php
namespace App\Service;

class TotpService
{
    private const DIGITS    = 6;
    private const PERIOD    = 30;
    private const ALGORITHM = 'sha1';
    private const WINDOW    = 2; // ±2 périodes de tolérance (±60 sec)

    public function verify(string $secret, int $code): bool
    {
        $timestamp = time();
        for ($i = -self::WINDOW; $i <= self::WINDOW; $i++) {
            $t = (int) floor($timestamp / self::PERIOD) + $i;
            if ($this->generate($secret, $t) === $code) return true;
        }
        return false;
    }

    private function generate(string $secret, int $counter): int
    {
        $key  = $this->base32Decode($secret);
        $time = pack('N*', 0) . pack('N*', $counter);
        $hash = hash_hmac(self::ALGORITHM, $time, $key, true);
        $off  = ord($hash[19]) & 0xf;
        $code = (
            ((ord($hash[$off])   & 0x7f) << 24) |
            ((ord($hash[$off+1]) & 0xff) << 16) |
            ((ord($hash[$off+2]) & 0xff) << 8)  |
            ( ord($hash[$off+3]) & 0xff)
        ) % (10 ** self::DIGITS);
        return $code;
    }

    private function base32Decode(string $input): string
    {
        $map   = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
        $input = strtoupper(str_replace('=', '', $input));
        $bits  = '';
        foreach (str_split($input) as $ch) {
            $pos   = strpos($map, $ch);
            if ($pos === false) continue;
            $bits .= str_pad(decbin($pos), 5, '0', STR_PAD_LEFT);
        }
        $bytes = '';
        foreach (str_split($bits, 8) as $byte) {
            if (strlen($byte) === 8) $bytes .= chr(bindec($byte));
        }
        return $bytes;
    }
}