import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:shimmer/shimmer.dart';
import 'package:url_launcher/url_launcher.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';
import '../widgets/product_detail_screen.dart';

class DiscoverScreen extends StatefulWidget {
  const DiscoverScreen({super.key});

  @override
  State<DiscoverScreen> createState() => _DiscoverScreenState();
}

class _DiscoverScreenState extends State<DiscoverScreen> {
  List<Map<String, dynamic>> products = [];
  bool isLoading = true;
  String selectedCategory = 'All';

  final List<String> categories = [
    'All',
    'Dresses',
    'Outerwear',
    'Accessories',
    'Shoes',
    'Bags',
  ];

  @override
  void initState() {
    super.initState();
    _loadProducts();
  }

  Future<void> _loadProducts() async {
    final data = await ApiService.fetchProducts();
    setState(() {
      products = data;
      isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            // Header
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Discover',
                      style: GoogleFonts.dmSans(
                        fontSize: 34,
                        fontWeight: FontWeight.w300,
                        letterSpacing: -1.2,
                        color: AppTheme.warmCream,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Curated by AI, styled for you',
                      style: GoogleFonts.dmSans(
                        fontSize: 14,
                        color: AppTheme.muted,
                      ),
                    ),
                    const SizedBox(height: 24),

                    // Search bar
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 16, vertical: 12),
                      decoration: BoxDecoration(
                        color: AppTheme.cardBg,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                            color: Colors.white.withValues(alpha: 0.06)),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.search_rounded,
                              color: AppTheme.muted.withValues(alpha: 0.6),
                              size: 20),
                          const SizedBox(width: 12),
                          Text(
                            'Search looks, brands, styles...',
                            style: TextStyle(
                              color: AppTheme.muted.withValues(alpha: 0.5),
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],
                ),
              ),
            ),

            // Category chips
            SliverToBoxAdapter(
              child: SizedBox(
                height: 40,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  itemCount: categories.length,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    final isSelected = categories[index] == selectedCategory;
                    return GestureDetector(
                      onTap: () =>
                          setState(() => selectedCategory = categories[index]),
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 200),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 20, vertical: 10),
                        decoration: BoxDecoration(
                          color: isSelected
                              ? AppTheme.warmCream
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                            color: isSelected
                                ? AppTheme.warmCream
                                : Colors.white.withValues(alpha: 0.12),
                          ),
                        ),
                        child: Text(
                          categories[index],
                          style: GoogleFonts.dmSans(
                            color: isSelected
                                ? AppTheme.scaffoldBg
                                : AppTheme.muted,
                            fontSize: 13,
                            fontWeight:
                                isSelected ? FontWeight.w600 : FontWeight.w400,
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ),

            const SliverToBoxAdapter(child: SizedBox(height: 24)),

            // Featured section
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 3,
                          height: 16,
                          decoration: BoxDecoration(
                            color: AppTheme.warmGold,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Text(
                          'TRENDING NOW',
                          style: GoogleFonts.dmSans(
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 2,
                            color: AppTheme.warmGold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                  ],
                ),
              ),
            ),

            // Product grid
            isLoading
                ? SliverToBoxAdapter(child: _buildLoadingGrid())
                : SliverPadding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    sliver: SliverGrid(
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 2,
                        mainAxisSpacing: 16,
                        crossAxisSpacing: 14,
                        childAspectRatio: 0.62,
                      ),
                      delegate: SliverChildBuilderDelegate(
                        (context, index) {
                          final product = products[index % products.length];
                          return _ProductTile(
                            product: product,
                            index: index,
                          );
                        },
                        childCount: 6,
                      ),
                    ),
                  ),

            const SliverToBoxAdapter(child: SizedBox(height: 40)),

            // Editorial section
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 3,
                          height: 16,
                          decoration: BoxDecoration(
                            color: AppTheme.softRose,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Text(
                          'EDITORIAL PICKS',
                          style: GoogleFonts.dmSans(
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 2,
                            color: AppTheme.softRose,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    _EditorialCard(
                      title: 'The Return of Quiet Luxury',
                      subtitle: 'Understated elegance for the modern wardrobe',
                      gradient: [
                        AppTheme.warmGold.withValues(alpha: 0.3),
                        AppTheme.cardBg,
                      ],
                    ),
                    const SizedBox(height: 12),
                    _EditorialCard(
                      title: 'Summer Statement Pieces',
                      subtitle: 'Bold moves for warm weather dressing',
                      gradient: [
                        AppTheme.softRose.withValues(alpha: 0.25),
                        AppTheme.cardBg,
                      ],
                    ),
                  ],
                ),
              ),
            ),

            const SliverToBoxAdapter(child: SizedBox(height: 100)),
          ],
        ),
      ),
    );
  }

  Widget _buildLoadingGrid() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: GridView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 16,
          crossAxisSpacing: 14,
          childAspectRatio: 0.62,
        ),
        itemCount: 4,
        itemBuilder: (context, index) {
          return Shimmer.fromColors(
            baseColor: AppTheme.cardBg,
            highlightColor: const Color(0xFF222222),
            child: Container(
              decoration: BoxDecoration(
                color: AppTheme.cardBg,
                borderRadius: BorderRadius.circular(14),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _ProductTile extends StatelessWidget {
  final Map<String, dynamic> product;
  final int index;

  const _ProductTile({required this.product, required this.index});

  @override
  Widget build(BuildContext context) {
    final List<String> placeholderImages = [
      'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400',
      'https://images.unsplash.com/photo-1539109136881-3be0616acf4b?w=400',
      'https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=400',
      'https://images.unsplash.com/photo-1581044777550-4cfa60707998?w=400',
      'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=400',
      'https://images.unsplash.com/photo-1487222477894-8943e31ef7b2?w=400',
    ];

    final imageUrl = placeholderImages[index % placeholderImages.length];
    final itemName = product['itemName'] ?? 'Fashion Item';
    final modelName = product['modelName'] ?? 'Meshada';

    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ProductDetailScreen(product: product, imageUrl: imageUrl),
          ),
        );
      },
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Image
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: CachedNetworkImage(
                imageUrl: imageUrl,
                fit: BoxFit.cover,
                width: double.infinity,
                placeholder: (_, __) => Shimmer.fromColors(
                  baseColor: AppTheme.cardBg,
                  highlightColor: const Color(0xFF222222),
                  child: Container(color: AppTheme.cardBg),
                ),
                errorWidget: (_, __, ___) => Container(
                  color: AppTheme.cardBg,
                  child: const Center(
                    child: Icon(Icons.broken_image_outlined,
                        color: AppTheme.muted, size: 32),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 10),
          Text(
            itemName,
            style: GoogleFonts.dmSans(
              color: AppTheme.warmCream,
              fontSize: 13,
              fontWeight: FontWeight.w500,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 2),
          Text(
            'by $modelName',
            style: const TextStyle(color: AppTheme.muted, fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _EditorialCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final List<Color> gradient;

  const _EditorialCard({
    required this.title,
    required this.subtitle,
    required this.gradient,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: gradient,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.dmSans(
                    color: AppTheme.warmCream,
                    fontSize: 17,
                    fontWeight: FontWeight.w600,
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  subtitle,
                  style: const TextStyle(
                    color: AppTheme.muted,
                    fontSize: 13,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Colors.white.withValues(alpha: 0.08),
            ),
            child: const Icon(Icons.arrow_forward_rounded,
                color: AppTheme.warmCream, size: 18),
          ),
        ],
      ),
    );
  }
}
