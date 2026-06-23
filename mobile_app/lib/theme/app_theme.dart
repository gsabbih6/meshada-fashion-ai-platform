import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  // Warm neutral palette — editorial luxury, not "AI dark mode"
  static const Color scaffoldBg = Color(0xFF0A0A0A);
  static const Color cardBg = Color(0xFF141414);
  static const Color warmCream = Color(0xFFF5E6D3);
  static const Color warmGold = Color(0xFFD4A574);
  static const Color softRose = Color(0xFFE8B4B4);
  static const Color muted = Color(0xFF8A8A8A);
  static const Color divider = Color(0xFF1E1E1E);

  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      scaffoldBackgroundColor: scaffoldBg,
      primaryColor: warmCream,
      colorScheme: const ColorScheme.dark(
        primary: warmCream,
        secondary: warmGold,
        surface: cardBg,
        onPrimary: scaffoldBg,
        onSecondary: scaffoldBg,
        onSurface: warmCream,
      ),
      textTheme: GoogleFonts.dmSansTextTheme(
        const TextTheme(
          displayLarge: TextStyle(
            fontSize: 34,
            fontWeight: FontWeight.w300,
            letterSpacing: -1.2,
            color: warmCream,
          ),
          displayMedium: TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.w400,
            letterSpacing: -0.8,
            color: warmCream,
          ),
          headlineMedium: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w500,
            letterSpacing: -0.4,
            color: warmCream,
          ),
          titleLarge: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.2,
            color: warmCream,
          ),
          titleMedium: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w500,
            color: warmCream,
          ),
          bodyLarge: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w400,
            color: warmCream,
            height: 1.5,
          ),
          bodyMedium: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w400,
            color: muted,
            height: 1.4,
          ),
          labelLarge: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.8,
            color: scaffoldBg,
          ),
          labelSmall: TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w500,
            letterSpacing: 0.6,
            color: muted,
          ),
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: scaffoldBg,
        elevation: 0,
        scrolledUnderElevation: 0,
        titleTextStyle: GoogleFonts.dmSans(
          fontSize: 18,
          fontWeight: FontWeight.w600,
          color: warmCream,
          letterSpacing: -0.2,
        ),
      ),
    );
  }
}
